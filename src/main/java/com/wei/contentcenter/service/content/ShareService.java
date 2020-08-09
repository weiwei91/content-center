package com.wei.contentcenter.service.content;

import com.wei.contentcenter.dao.content.ShareMapper;
import com.wei.contentcenter.domain.dto.content.ShareAuditDTO;
import com.wei.contentcenter.domain.dto.content.ShareDTO;
import com.wei.contentcenter.domain.dto.user.UserDTO;
import com.wei.contentcenter.domain.entity.content.Share;
import com.wei.contentcenter.domain.enums.AuditStatusEnum;
import com.wei.contentcenter.domain.messaging.UserAddBonusMsgDTO;
import com.wei.contentcenter.feignclient.UserCenterFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShareService {
    private final ShareMapper shareMapper;
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final UserCenterFeignClient userCenterFeignClient;
    private final RocketMQTemplate rocketMQTemplate;

    public ShareDTO findById(Integer id) {
        // 获取分享详情
        Share share = this.shareMapper.selectByPrimaryKey(id);
        // 发布人id
        Integer userId = share.getUserId();
//
//        UserDTO userDTO = restTemplate.getForObject("http://localhost:8010/users/{id}",
//                UserDTO.class, userId);
//代码改进，增加服务发现
        List<ServiceInstance> instances = discoveryClient.getInstances("user-center");
//        String targetUrl =  instances.stream()
//                //数据变换
//                .map(instance -> instance .getUri().toString() + "/users/{id}" )
//                .findFirst()
//                .orElseThrow(()-> new IllegalArgumentException("当前没有实例"));

        // 1. 代码不可读
        // 2. 复杂的url难以维护：https://user-center/s?ie={ie}&f={f}&rsv_bp=1&rsv_idx=1&tn=baidu&wd=a&rsv_pq=c86459bd002cfbaa&rsv_t=edb19hb%2BvO%2BTySu8dtmbl%2F9dCK%2FIgdyUX%2BxuFYuE0G08aHH5FkeP3n3BXxw&rqlang=cn&rsv_enter=1&rsv_sug3=1&rsv_sug2=0&inputT=611&rsv_sug4=611
        // 3. 难以相应需求的变化，变化很没有幸福感
        // 4. 编程体验不统一
        //String targetUrl = "http://user-center/users/{userId}";
        // UserDTO userDTO = restTemplate.getForObject(targetUrl, UserDTO.class, userId);
        ShareDTO shareDTO = new ShareDTO();

        UserDTO userDTO = this. userCenterFeignClient.findById(userId);

        //用fein
        // 消息的装配
        BeanUtils.copyProperties(share, shareDTO);
        shareDTO.setWxNickname(userDTO.getWxNickname());

        return shareDTO;
    }



    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        // 用HTTP GET方法去请求，并且返回一个对象
        UserDTO userDTO = restTemplate.getForObject("http://localhost:8010/users/{id}",
                UserDTO.class, 1);


        //System.out.println(forEntity.getBody());
        // 200 OK
        // 500
        // 502 bad gateway...
        //System.out.println(forEntity.getStatusCode());
    }

    public Share auditById(Integer id, ShareAuditDTO auditDTO) {
        // 1. 查询share是否存在，不存在或者当前的audit_status != NOT_YET，那么抛异常
        Share share = this.shareMapper.selectByPrimaryKey(id);
        if (share == null) {
            throw new IllegalArgumentException("参数非法！该分享不存在！");
        }
        if (!Objects.equals("NOT_YET", share.getAuditStatus())) {
            throw new IllegalArgumentException("参数非法！该分享已审核通过或审核不通过！");
        }

        // 3. 如果是PASS，那么发送消息给rocketmq，让用户中心去消费，并为发布人添加积分
        this.rocketMQTemplate.convertAndSend("add-bonus",
                UserAddBonusMsgDTO.builder()
                .userId(share.getUserId())
                .bonus(50)
                .build()
        );
        return share;
    }
}

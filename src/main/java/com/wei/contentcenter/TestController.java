package com.wei.contentcenter;

import com.wei.contentcenter.dao.content.ShareMapper;
import com.wei.contentcenter.domain.entity.content.Share;
import com.wei.contentcenter.feignclient.TestBaiduFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
public class TestController {
    /**
     * // 作业1：课后研究一下@Resource和@Autowired的区别
     * // 面试题
     */
    @Resource
    private ShareMapper shareMapper;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private TestBaiduFeignClient testBaiduFeignClient;

    @GetMapping("/test")
    public List<Share> testInsert() {
        // 1. 做插入
        Share share = new Share();
        share.setCreateTime(new Date());
        share.setUpdateTime(new Date());
        share.setTitle("xxx");
        share.setCover("xxx");
        share.setAuthor("大目");
        share.setBuyCount(1);

        this.shareMapper.insertSelective(share);

        // 2. 做查询: 查询当前数据库所有的share  select * from share ;
        List<Share> shares = this.shareMapper.selectAll();

        return shares;
    }


    /**
     * 测试：服务发现，证明内容中心总能找到用户中心
     *
     * @return 用户中心所有实例的地址信息
     */
    @GetMapping("/test2")
    public List<ServiceInstance> getInstances() {
        // 查询指定服务的所有实例的信息
        // consul/eureka/zookeeper...
        return this.discoveryClient.getInstances("user-center");
    }

    @GetMapping("/baidu")
    public String baiduIndex() {
        return this.testBaiduFeignClient.index();
    }


}

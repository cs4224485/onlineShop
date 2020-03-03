package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.user.mapper.UserMemberReceiveAddressMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserMemberReceiveAddressMapper userMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMemberList = userMapper.selectAllUser();
        return umsMemberList;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressesList = userMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddressesList;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String umsMemberStr = jedis.get("user:" + umsMember.getUsername() +":" +umsMember.getPassword() + ":info");
                if (StringUtils.isNotBlank(umsMemberStr)) {
                    // 密码正确
                    UmsMember umsMemberFormCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    System.out.println(umsMemberFormCache);
                    return umsMemberFormCache;

                }
            }
            // redis失效 开启数据库
            // 密码错误或者换成中没有
            UmsMember umsMemberFromDb = loginFromDb(umsMember);
            if (umsMemberFromDb != null && jedis != null) {
                jedis.setex("user:" + umsMember.getUsername() +":" +umsMember.getPassword() + ":info", 60 * 60 * 24, JSON.toJSONString(umsMemberFromDb));
            } else {
                return null;
            }
            return umsMemberFromDb;

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    private UmsMember loginFromDb(UmsMember umsMember) {

        List<UmsMember> umsMembers = userMapper.select(umsMember);
        System.out.println(umsMembers);
        if (umsMembers.size() != 0) {
            return umsMembers.get(0);
        }

        return null;
    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        try {
            jedis.setex("user:" + memberId + ":token", 60 * 60 * 2, token);
        } finally {
            jedis.close();
        }
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return umsMember;
    }
    @Override
        public UmsMember checkOauthUser(UmsMember umsCheck) {
            UmsMember umsMember = userMapper.selectOne(umsCheck);
            return umsMember;
        }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress1 = userMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
        return umsMemberReceiveAddress1;
    }
}

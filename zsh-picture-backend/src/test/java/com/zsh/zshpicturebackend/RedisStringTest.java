package com.zsh.zshpicturebackend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
public class RedisStringTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedisStringOperations() {
        // 获取操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        // key和value
        String key = "testKey";
        String value = "testValue";

        // 1.测试新增
        valueOps.set(key, value);
        String storedValue = valueOps.get(key);
        Assertions.assertEquals(value, storedValue, "存储的值与预期的不一致");

        // 2.测试修改
        String updatedValue = "updatedValue";
        valueOps.set(key, updatedValue);
        storedValue = valueOps.get(key);
        Assertions.assertEquals(updatedValue, storedValue, "修改后的值与预期的不一致");

        // 3.测试查询
        storedValue = valueOps.get(key);
        Assertions.assertNotNull(storedValue, "查询的值为空");
        Assertions.assertEquals(updatedValue, storedValue, "查询的值与预期的不一致");

        // 4.测试删除
        stringRedisTemplate.delete(key);
        storedValue = valueOps.get(key);
        Assertions.assertNull(storedValue, "删除后的值不为空");
    }
}

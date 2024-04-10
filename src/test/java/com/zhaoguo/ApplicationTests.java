package com.zhaoguo;

import com.zhaoguo.entity.Blog;
import com.zhaoguo.entity.User;
import com.zhaoguo.mapper.BlogMapper;
import com.zhaoguo.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTests {
    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void testBlogMapper() {
        Blog blog = blogMapper.selectById(4);
        System.out.println("blog = " + blog);
    }

}

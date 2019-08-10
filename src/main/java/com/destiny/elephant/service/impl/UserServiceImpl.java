package com.destiny.elephant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.destiny.elephant.mapper.UserMapper;
import com.destiny.elephant.entity.Role;
import com.destiny.elephant.entity.User;
import com.destiny.elephant.service.UserService;
import com.destiny.elephant.util.ElephantValidationUtil;
import com.destiny.elephant.util.ToolUtil;
import com.google.common.collect.Maps;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author wangl
 * @since 2017-10-31
 */
@Service("userService")
@Transactional(readOnly = true, rollbackFor = Exception.class)
@Lazy(value = false)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /* 这里caching不能添加put 因为添加了总会执行该方法
     * @see com.mysiteforme.service.UserService#findUserByLoginName(java.lang.String)
     * type: 1 邮箱 2 cellphone 3 账户名
     */
    @Cacheable(value = "user", key = "'user_name_'+#name", unless = "#result == null")
    @Override
    public User findUserByLoginName(String name) {
        User user = new User();
        if (ElephantValidationUtil.isEmail(name)) {
            user.setEmail(name);
        } else if (ElephantValidationUtil.isMobile(name)) {
            user.setTel(name);
        } else {
            user.setLoginName(name);
        }

        return baseMapper.selectUserByEntity(user);
    }


    @Cacheable(value = "user", key = "'user_id_'+T(String).valueOf(#id)", unless = "#result == null")
    @Override
    public User findUserById(Long id) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("id", id);
        return baseMapper.selectUserByMap(map);
    }

    @Override
    @Caching(put = {
            @CachePut(value = "user", key = "'user_id_'+T(String).valueOf(#result.id)", condition = "#result.id != null and #result.id != 0"),
            @CachePut(value = "user", key = "'user_name_'+#user.loginName", condition = "#user.loginName !=null and #user.loginName != ''"),
            @CachePut(value = "user", key = "'user_email_'+#user.email", condition = "#user.email != null and #user.email != ''"),
            @CachePut(value = "user", key = "'user_tel_'+#user.tel", condition = "#user.tel != null and #user.tel != ''")
    })
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public User saveUser(User user) {
        ToolUtil.encryptPassword(user);
        user.setLocked(false);
        baseMapper.insert(user);
        //保存用户角色关系
        this.saveUserRoles(user.getId(), user.getRoleLists());
        return findUserById(user.getId());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "user", key = "'user_id_'+T(String).valueOf(#user.id)", condition = "#user.id != null and #user.id != 0"),
            @CacheEvict(value = "user", key = "'user_name_'+#user.loginName", condition = "#user.loginName !=null and #user.loginName != ''"),
            @CacheEvict(value = "user", key = "'user_email_'+#user.email", condition = "#user.email != null and #user.email != ''"),
            @CacheEvict(value = "user", key = "'user_tel_'+#user.tel", condition = "#user.tel != null and #user.tel != ''"),
    })
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public User updateUser(User user) {
        baseMapper.updateById(user);
        //先解除用户跟角色的关系
        this.dropUserRolesByUserId(user.getId());
        this.saveUserRoles(user.getId(), user.getRoleLists());
        return user;
    }

    @Transactional(readOnly = false, rollbackFor = Exception.class)
    @Override
    public void saveUserRoles(Long id, Set<Role> roleSet) {
        baseMapper.saveUserRoles(id, roleSet);
    }

    @Transactional(readOnly = false, rollbackFor = Exception.class)
    @Override
    public void dropUserRolesByUserId(Long id) {
        baseMapper.dropUserRolesByUserId(id);
    }

    @Override
    public int userCount(String param) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", param).or().eq("email", param).or().eq("tel", param);
        int count = baseMapper.selectCount(wrapper);
        return count;
    }

    @Transactional(readOnly = false, rollbackFor = Exception.class)
    @Override
    @Caching(evict = {
            @CacheEvict(value = "user", key = "'user_id_'+T(String).valueOf(#user.id)", condition = "#user.id != null and #user.id != 0"),
            @CacheEvict(value = "user", key = "'user_name_'+#user.loginName", condition = "#user.loginName !=null and #user.loginName != ''"),
            @CacheEvict(value = "user", key = "'user_email_'+#user.email", condition = "#user.email != null and #user.email != ''"),
            @CacheEvict(value = "user", key = "'user_tel_'+#user.tel", condition = "#user.tel != null and #user.tel != ''")
    })
    public void deleteUser(User user) {
        user.setDelFlag(true);
        user.updateById();
    }

    /**
     * 查询用户拥有的每个菜单具体数量
     *
     * @return
     */
    @Override
    public Map selectUserMenuCount() {
        return baseMapper.selectUserMenuCount();
    }

}
package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.request.RegisterOwnerRequest;
import com.example.turf_Backend.dto.request.RegistercustomerRequest;
import com.example.turf_Backend.dto.response.AuthResponse;
import com.example.turf_Backend.enums.Role;
import com.example.turf_Backend.enums.Status;
import com.example.turf_Backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {


public User toUserEntity(RegistercustomerRequest dto)
{
    User user=new User();
    user.setName(dto.getName());
    user.setEmail(dto.getEmail());
    user.setPassword(dto.getPassword());
    user.setRole(Role.CUSTOMER);
    return  user;
}
public User toOwnerEntity(RegisterOwnerRequest dto)
{
    User user=new User();
    user.setName(dto.getName());
    user.setEmail(dto.getEmail());
    user.setPassword(dto.getPassword());
    user.setRole(Role.OWNER);
    user.setSubscriptionStatus(Status.PENDING);
    user.setSubscriptionAmount(dto.getSubscriptionAmount());
    return  user;
}
public AuthResponse toResponse(User user,String token,String message)
{
    return new AuthResponse(token,user.getRole().name(),message);
}

}

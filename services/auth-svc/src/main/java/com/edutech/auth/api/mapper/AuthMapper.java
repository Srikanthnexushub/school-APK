// src/main/java/com/edutech/auth/api/mapper/AuthMapper.java
package com.edutech.auth.api.mapper;

import com.edutech.auth.application.dto.UserResponse;
import com.edutech.auth.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    UserResponse toUserResponse(User user);
}

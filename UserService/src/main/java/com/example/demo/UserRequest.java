package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest implements Serializable {
    private String userId;
    private String userName;
    private String email;
    private String requestType;
    private long requestTimestamp;
    private String requestOrigin;
}



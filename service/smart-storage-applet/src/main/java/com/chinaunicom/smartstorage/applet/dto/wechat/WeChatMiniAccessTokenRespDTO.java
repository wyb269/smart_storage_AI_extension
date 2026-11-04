package com.chinaunicom.smartstorage.applet.dto.wechat;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liuchink
 * @since 2023/11/3 15:52
 */
@Data
public class WeChatMiniAccessTokenRespDTO  {


    @JSONField(name = "access_token")
    private String accessToken;
    @JSONField(name = "expires_in")
    private Integer expiresIn;
    private LocalDateTime addTime;
}

package com.chinaunicom.smartstorage.common.enumbean;

import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import lombok.Getter;

@Getter
public enum FileDomain {

    DEFAULT("default", "默认路径");

    private final String code;
    private final String description;

    FileDomain(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static FileDomain fromCode(String code) {
        for (FileDomain status : FileDomain.values()) {
            if (status.getCode()
                    .equals(code)) {
                return status;
            }
        }
        throw new CustomException("业务域不合法");
    }
}

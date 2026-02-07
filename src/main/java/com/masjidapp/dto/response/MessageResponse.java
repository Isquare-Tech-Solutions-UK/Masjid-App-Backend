package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    @JsonProperty("message")
    private String message;

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }

}

package com.verify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class VerimailResponse {

    private String status;
    private String email;
    private String result;
    private Boolean deliverable;
    private String user;
    private String domain;
    @JsonProperty("did_you_mean")
    private String didYouMean;
}

package com.verify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class EmailValidation {
    private String email;
    private String autocorrect;
    private String deliverability;
    @JsonProperty("quality_score")
    private String qualityScore;
    @JsonProperty("is_valid_format")
    private IsValidFormat isValidFormat;
    @JsonProperty("is_free_email")
    private IsFreeEmail isFreeEmail;
    @JsonProperty("is_disposable_email")
    private IsDisposableEmail isDisposableEmail;
    @JsonProperty("is_role_email")
    private IsRoleEmail isRoleEmail;
    @JsonProperty("is_catchall_email")
    private IsCatchallEmail isCatchallEmail;
    @JsonProperty("is_mx_found")
    private IsMxFound isMxFound;
    @JsonProperty("is_smtp_valid")
    private IsSmtpValid isSmtpValid;
}

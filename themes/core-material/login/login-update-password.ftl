<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
                    <div class="form-group">
                        <label for="password-new" class="${properties.kcLabelClass!}">${msg("passwordNew")}</label>
                        <input type="password" id="password-new" name="password-new" class="form-control"
                               autofocus autocomplete="new-password"
                               aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                        />

                        <#if messagesPerField.existsError('password')>
                            <span id="input-error-password" class="alert-error" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('password'))?no_esc}
                            </span>
                        </#if>
                    </div>

                    <div class="form-group">
                        <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
                        <input type="password" id="password-confirm" name="password-confirm"
                               class="form-control" autocomplete="new-password"
                               aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                        />

                        <#if messagesPerField.existsError('password-confirm')>
                            <span id="input-error-password-confirm" class="alert-error" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                            </span>
                        </#if>
                    </div>

                    <div id="kc-form-buttons">
                        <#if isAppInitiatedAction??>
                            <input class="btn btn-primary" type="submit" value="${msg("doSubmit")}" />
                            <button class="btn btn-default" type="submit" name="cancel-aia" value="true" />${msg("doCancel")}</button>
                        <#else>
                            <input class="btn btn-primary" type="submit" value="${msg("doSubmit")}" />
                        </#if>
                    </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
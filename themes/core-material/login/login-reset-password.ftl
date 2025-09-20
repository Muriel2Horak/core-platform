<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="mdc-text-field mdc-text-field--outlined ${properties.kcFormGroupClass!}">
                <input type="text" id="username" name="username" class="mdc-text-field__input ${properties.kcInputClass!}" 
                       autofocus value="${(auth.attemptedUsername!'')}" 
                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" />
                <div class="mdc-notched-outline">
                    <div class="mdc-notched-outline__leading"></div>
                    <div class="mdc-notched-outline__notch">
                        <label class="mdc-floating-label" for="username">
                            <#if !realm.loginWithEmailAllowed>${msg("username")}
                            <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                            <#else>${msg("email")}</#if>
                        </label>
                    </div>
                    <div class="mdc-notched-outline__trailing"></div>
                </div>
                <#if messagesPerField.existsError('username')>
                    <div class="mdc-text-field-helper-line">
                        <div class="mdc-text-field-helper-text mdc-text-field-helper-text--validation-msg">
                            ${kcSanitize(messagesPerField.get('username'))?no_esc}
                        </div>
                    </div>
                </#if>
            </div>

            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}" class="mdc-button mdc-button--text">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <button class="mdc-button mdc-button--raised mdc-button--primary ${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit">
                        <div class="mdc-button__ripple"></div>
                        <span class="mdc-button__label">${msg("doSubmit")}</span>
                    </button>
                </div>
            </div>
        </form>
    <#elseif section = "info" >
        <#if realm.duplicateEmailsAllowed>
            ${msg("emailInstructionUsername")}
        <#else>
            ${msg("emailInstruction")}
        </#if>
    </#if>
</@layout.registrationLayout>
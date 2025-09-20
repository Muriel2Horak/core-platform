<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}"<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width,initial-scale=1"/>

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    
    <!-- Material Design Icons -->
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
    
    <script>
        // Material Design Text Field activation
        document.addEventListener('DOMContentLoaded', function() {
            // Activate floating labels
            const textFields = document.querySelectorAll('.mdc-text-field');
            textFields.forEach(function(textField) {
                const input = textField.querySelector('.mdc-text-field__input');
                const label = textField.querySelector('.mdc-floating-label');
                
                if (input && label) {
                    // Check if input has value on load
                    if (input.value) {
                        label.classList.add('mdc-floating-label--float-above');
                    }
                    
                    // Handle focus events
                    input.addEventListener('focus', function() {
                        label.classList.add('mdc-floating-label--float-above');
                    });
                    
                    input.addEventListener('blur', function() {
                        if (!input.value) {
                            label.classList.remove('mdc-floating-label--float-above');
                        }
                    });
                }
            });
            
            // Activate buttons
            const buttons = document.querySelectorAll('.mdc-button');
            buttons.forEach(function(button) {
                button.addEventListener('click', function(e) {
                    // Add ripple effect
                    const ripple = button.querySelector('.mdc-button__ripple');
                    if (ripple) {
                        ripple.style.animation = 'none';
                        ripple.offsetHeight; // trigger reflow
                        ripple.style.animation = 'mdc-ripple-fg-radius-in 225ms, mdc-ripple-fg-opacity-in 75ms';
                    }
                });
            });
        });
    </script>
    
    <style>
        /* Additional Material Design animations */
        @keyframes mdc-ripple-fg-radius-in {
            from {
                animation-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
                transform: translate(var(--mdc-ripple-fg-translate-start, 0)) scale(1);
            }
            to {
                transform: translate(var(--mdc-ripple-fg-translate-end, 0)) scale(var(--mdc-ripple-fg-scale, 1));
            }
        }
        
        @keyframes mdc-ripple-fg-opacity-in {
            from {
                animation-timing-function: linear;
                opacity: 0;
            }
            to {
                opacity: var(--mdc-ripple-fg-opacity, 0);
            }
        }
        
        .mdc-floating-label--float-above {
            transform: translateY(-106%) scale(0.75);
            color: var(--primary-main);
        }
    </style>
</head>

<body class="${properties.kcBodyClass!}">
<div id="kc-container" class="${properties.kcContainerClass!}">
    <div id="kc-container-wrapper" class="${properties.kcContainerWrapperClass!}">

        <div id="kc-header" class="${properties.kcHeaderClass!}">
            <div id="kc-header-wrapper" class="${properties.kcHeaderWrapperClass!}">
                <#nested "header">
            </div>
        </div>

        <div id="kc-content">
            <div id="kc-content-wrapper">

                <#-- App-initiated actions should not see warning messages about the need to complete the action -->
                <#-- during login.                                                                               -->
                <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                    <div class="alert-${message.type} kc-feedback-text">
                        <#if message.type = 'success'><span class="material-icons">check_circle</span></#if>
                        <#if message.type = 'warning'><span class="material-icons">warning</span></#if>
                        <#if message.type = 'error'><span class="material-icons">error</span></#if>
                        <#if message.type = 'info'><span class="material-icons">info</span></#if>
                        <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                    </div>
                </#if>

                <div id="kc-form">
                    <div id="kc-form-wrapper">
                        <#nested "form">
                    </div>
                </div>

                <#if displayInfo>
                    <div id="kc-info" class="${properties.kcSignUpClass!}">
                        <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                            <#nested "info">
                        </div>
                    </div>
                </#if>
            </div>
        </div>

    </div>
</div>
</body>
</html>
</#macro>
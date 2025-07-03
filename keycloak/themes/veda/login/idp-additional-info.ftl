<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <form id="kc-idp-additional-info-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <label for="extraInfo" class="${properties.kcLabelClass!}">Extra Info</label>
            <input type="text" id="extraInfo" name="extraInfo" class="${properties.kcInputClass!}" autofocus />
        </div>
        <div class="${properties.kcFormGroupClass!}">
            <input type="submit" value="Continue" class="${properties.kcButtonClass!}" />
        </div>
    </form>
</@layout.registrationLayout> 
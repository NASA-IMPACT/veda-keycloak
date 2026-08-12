<#--
  Plain text alternative for the approval email. Keycloak requires both a
  text/ and an html/ template for every email it sends.
-->
<#if link??>${msg("approvalNotificationBody", link)}<#else>${msg("approvalNotificationBodyNoLink")}</#if>

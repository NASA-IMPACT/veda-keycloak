<#--
  Verification email, sent at registration (realm has verifyEmail=true).
  Inline styles and tables on purpose: email clients handle <style> poorly.
-->
<html>
<body style="margin:0;padding:0;background-color:#f0f0f0;">
  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="background-color:#f0f0f0;padding:32px 16px;">
    <tr>
      <td align="center">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="max-width:560px;background-color:#ffffff;border:1px solid #dfe1e2;border-radius:4px;">
          <tr>
            <td style="padding:32px 32px 0 32px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
              <p style="margin:0 0 24px 0;font-size:14px;font-weight:600;letter-spacing:0.02em;color:#0050d8;">EARTH INFORMATION EXPLORER</p>
              <h1 style="margin:0 0 16px 0;font-size:24px;line-height:1.3;font-weight:600;color:#1c1d1f;">Confirm your email address</h1>
              <p style="margin:0 0 16px 0;font-size:16px;line-height:1.6;color:#454749;">
                <#if user.firstName??>Hi ${user.firstName},<#else>Hello,</#if>
              </p>
              <p style="margin:0 0 24px 0;font-size:16px;line-height:1.6;color:#454749;">
                Thanks for requesting access to Earth Information Explorer. Confirm this email address to continue.
              </p>
            </td>
          </tr>
          <tr>
            <td style="padding:0 32px 24px 32px;">
              <a href="${link}" style="display:inline-block;padding:12px 24px;background-color:#0050d8;color:#ffffff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;font-size:16px;font-weight:600;text-decoration:none;border-radius:4px;">Confirm email address</a>
            </td>
          </tr>
          <tr>
            <td style="padding:0 32px 32px 32px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
              <p style="margin:0 0 16px 0;font-size:16px;line-height:1.6;color:#454749;">
                After you confirm, an administrator reviews your request — usually within two business days. Once that is done you will be able to sign in, so please try again after then.
              </p>
              <p style="margin:0 0 16px 0;font-size:14px;line-height:1.6;color:#71767a;">
                This link expires in ${linkExpirationFormatter(linkExpiration)}.
              </p>
              <p style="margin:0;padding-top:16px;border-top:1px solid #dfe1e2;font-size:14px;line-height:1.6;color:#71767a;">
                If you did not request an Earth Information Explorer account, you can ignore this message.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>

<!DOCTYPE html>
<html>
  <head>
    {{> partials/header }}
    <link rel="stylesheet" href="/s/css/landing.css?{VERSION}">
  </head>
  <body>
    <div id="accouts-div">
      <h1><a href="/"> Rssminer</a> Login </h1>
      {{#msg}}<p id="error">{{{ msg }}}</p>{{/msg}}
      <div class="openid s">
        <h3>Open ID login</h3>
        <a href="/login/google">
          <img src="/s/imgs/openid_google.png"/>
        </a>
      </div>
      <form action="/" method="post" class="post-form s">
        <h3>Password login</h3>
        <table>
          <tr>
            <td><label for="email">Email:</label>
            </td>
            <td><input class="textfield" name="email" id="email" />
              <span class="required">*</span>
            </td>
          </tr>
          <tr>
            <td><label for="password">Password:</label>
            </td>
            <td><input class="textfield" name="password"
                       id="password" type="password"/>
              <span class="required">*</span>
            </td>
          </tr>
          <tr>
            <td></td>
            <td>
              <input class="submit" type="submit" value="Login" />
            </td>
          </tr>
        </table>
        <input value="{{{return-url}}}" name="return-url" type="hidden" />
      </form>
      <div class="s">
        <h3>Try with a live demo</h3>
        <div id="demo">
          <a href="/demo">demo</a>
        </div>
      </div>
    </div>
  </body>
</html>

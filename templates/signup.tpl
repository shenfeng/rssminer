<!DOCTYPE html>
<html>
  <head>
    {{> partials/header }}
    <link rel="stylesheet" href="/s/css/login.css?{VERSION}">
  </head>
  <body>
    <div id="accouts-div">
      <h1> <a href="/">Rssminer</a> signup </h1>
      <form action="" method="post" class="post-form">
        {{#error}}<p>{{error}}</p>{{/error}}
        <table>
          <tr>
            <td><label for="email">Email: </label>
            </td>
            <td><input class="textfield" id="email" name="email"/>
              <span class="required">*</span>
            </td>
          </tr>
          <tr>
            <td><label for="password">Password:</label></td>
            <td><input class="textfield" name="password"
                       id="password" type="password"/>
              <span class="required">*</span>
            </td>
          </tr>
          <tr>
            <td></td>
            <td><input type="submit" value="Create" class="submit"/></td>
          </tr>
        </table>
      </form>
      <p>Already has an account? <a href="/">Login</a></p>
    </div>
  </body>
</html>

<div id="settings">
  <h2>Settings</h2>
  <table>
    <tr>
      <td>
        <h4>Password</h4>
      </td>
      <td>
        <p> <input type="password" placeholder="new password"
                   name="password"/> </p>
        <p> <input type="password" placeholder="retype password"
                   name="password2" id="password2"/> </p>
      </td>
    </tr>
    <tr>
      <td><h4>Expire time</h4></td>
      <td>
        <select name="expire">
          {{#expire_times}}
            {{#selected}}
              <option selected value="{{time}}">{{time}} day</option>
            {{/selected}}
            {{^selected}}
              <option value="{{time}}">{{time}} day</option>
            {{/selected}}
          {{/expire_times}}
        </select>
      </td>
    </tr>
  </table>

  <button id="save-settings">Save </button>
</div>


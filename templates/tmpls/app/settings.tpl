<div id="settings">
  <h2>Settings</h2>
  <section>
    <h4>Password</h4>
    <div>
      <input type="password" placeholder="new password"
      name="password"/>
      <input type="password" placeholder="retype password"
      name="password2" id="password2"/>
    </div>
  </section>
  <section>
    <h4>Expire time</h4>
    <div>
      <select name="expire"
        title="System automatically mark feed as read when
        expires, calculated from author publish it.
        Google Reader is 30 day.">
        {{#expire_times}}
          {{#selected}}
            <option selected value="{{time}}">{{time}} day</option>
          {{/selected}}
          {{^selected}}
          <option value="{{time}}">{{time}} day</option>
          {{/selected}}
        {{/expire_times}}
      </select>
    </div>
  </section>
  <section>
    <button id="save-settings">Save</button>
  </section>
  <table >
    <caption>Manager</caption>
    {{#groups}}
      {{#subs}}
        <tr data-id="{{id}}">
          <td><img width="16" height="16" src="{{img}}"/></td>
          <td class="title" title="{{title}}">{{ title }}</td>
          <td class="delete" title="unsubscribe">X</td>
        </tr>
      {{/subs}}
    {{/groups}}
  </table>
</div>

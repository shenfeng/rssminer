<h2>
  Settings - Rssminer
</h2>
<div class="sort settings-sort">
  <ul>
    <li class="selected">add</li>
    <li>account</li>
  </ul>
</div>
<ul id="all-settings" class="show-add">
  <li class="section add-sub">
    <div id="add-sub">
      <section>
        <h4>Import</h4>
        <div>
          <p>
            <a href="/import/google">
            <img src="/imgs/import-greader.png"/>
            </a>
          </p>
        </div>
      </section>
      <section>
        <h4>URL</h4>
        <div>
          <input id="rss_atom_url" placeholder="atom/rss url"/>
        </div>
      </section>
      <section>
        <button id="add-subscription">add</button>
      </section>
    </div>
  </li>
  <li class="section account">
    <div id="account">
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
          <span class="tip">System automatically mark feed as read when
            expires, calculated from author publish it.
            Google Reader is 30 day.</span>
        </div>
      </section>
      <section>
        <button id="save-settings">Save</button>
      </section>
    </div>
  </li>
</ul>




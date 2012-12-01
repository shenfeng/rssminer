<h2> {{ title }} - Rssminer </h2>
<div class="sort">
  <ul>
    {{#tabs}}
      <li {{#s}} class="selected" {{/s}}>
        <a href="#s/{{ n }}">{{text}}</a>
      </li>
    {{/tabs}}
  </ul>
</div>
<ul id="all-settings" class="show-{{selected}}">
  <li class="section add-sub">
    {{#demo}}
      <p class="warn">{{{m_demo_add_warn}}}</p>
    {{/demo}}
    <div>
      <h4>{{m_import}}</h4>
      <div>
        <p>
          <a href="/import/google" class="import" data-title="{{m_import_grader}}">
          <img src="/s/imgs/import-greader.png"/>
          </a>
        </p>
      </div>
    </div>
    <div>
      <h4>{{m_url}}</h4>
      <div>
        <input id="rss_atom_url" placeholder="{{m_paste_url}}"/>
        <ul class="refs">
          <li>
            <a target="_blank"
              data-title="RSS - Wikipedia, the free encyclopedia"
              href="http://en.wikipedia.org/wiki/RSS">
              <img src="/s/imgs/wiki.ico"/>
            </a>
          </li>
          <li>
            <a target="_blank" data-title="什么是RSS？百度百科相关词条解释，点击查看"
              href="http://baike.baidu.com/view/1644.htm">
              <img src="/s/imgs/bk.ico"/>
            </a>
          </li>
        </ul>
      </div>
    </div>
    <div>
      <button id="add-subscription">{{m_add}}</button>
    </div>
  </li>
  <li class="section settings">
    <div>
      <p>{{m_default_list}}</p>
      <select>
        {{#sortings}}
          <option {{#s}}selected{{/s}} value="{{value}}">{{text}}</option>
        {{/sortings}}
      </select>
    </div>
    <div>
      <p class="warn">{{m_set_pass_p1}}</p>
      <p>{{m_set_pass_p2}}</p>
      <p>{{m_set_pass_p3}}</p>
      <h4>{{m_password}}</h4>
      <div>
        <input type="password" placeholder="{{m_password}}" name="password"/>
        <br>
        <input type="password" placeholder="{{m_password_again}}" name="password2" id="password2"/>
      </div>
    </div>
    <div>
      <button id="save-settings">{{m_save}}</button>
    </div>
  </li>
</ul>

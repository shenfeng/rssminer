<ul>
  {{#navs}}
    <li class="cat">
      {{#href}}<a href="{{href}}">{{/href}}
        <div class="item">
          <span class="text">{{text}}</span>
          {{#c}}<span class="count">{{c}}</span>{{/c}}
        </div>
      {{#href}}</a>{{/href}}
      {{#has_sub}}
        <ul>
          {{#subs}}
            <li class="sub">
              <a href="{{href}}">
                <div class="item" title="{{text}}">
                  <span class="text">{{text}}</span>
                  {{#c}}
                    <span class="count">{{c}}</span>
                  {{/c}}
                </div>
              </a>
            </li>
          {{/subs}}
        </ul>
      {{/has_sub}}
    </li>
  {{/navs}}
</ul>

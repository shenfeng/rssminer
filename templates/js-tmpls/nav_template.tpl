<ul class="nav-tree">
  {{#data}}
    <li class="folder unread">
      <span class="toggle icon"></span>
      <h4 href="#">{{group_name}}</h4>
      <ul >
        {{#subscriptions}}
          <li class="sub">
            {{#favicon}}
              <img src="{{favicon}}" class="favicon"/>
            {{/favicon}}
            {{^favicon}}
            <img src="/imgs/16px-feed-icon.png?{VERSION}"
            class="favicon"/>
            {{/favicon}}
            <a id="subs-{{id}}" href="#/subscription/{{id}}">{{title}} </a>
            <div class="icon"/>
          </li>
        {{/subscriptions}}
      </ul>
    </li>
  {{/data}}
</ul>

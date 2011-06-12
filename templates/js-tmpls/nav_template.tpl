<ul class="nav-tree">
  {{#each this}}
  <li class="folder unread">
    <span class="toggle icon"></span>
    <a href="#">{{group_name}}</a>
    <ul >
      {{#each subscriptions}}
      <li class="sub">
        {{#if favicon}}
        <img src="{{favicon}}" class="favicon"/>
        {{else}}
        <img src="/imgs/16px-feed-icon.png?{VERSION}" class="favicon"/>
        {{/if}}
        <a id="subs-{{id}}" href="#/subscription/{{id}}">{{title}} </a>
      </li>
      {{/each}}
    </ul>
  </li>
  {{/each}}
</ul>

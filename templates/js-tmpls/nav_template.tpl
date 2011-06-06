<ul class="nav-tree">
  {{#each this}}
  <li class="folder unread">
    <span class="toggle icon"></span>
    <a href="#">{{group_name}}</a>
    <ul >
      {{#each subscriptions}}
      <li class="sub">
        <img src="{{favicon}}" class="favicon"/>
        <a href="#">{{title}} </a>
      </li>
      {{/each}}
    </ul>
  </li>
  {{/each}}
</ul>

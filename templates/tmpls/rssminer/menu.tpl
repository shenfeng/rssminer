<ul class="menu-tree">
  {{#menus}}
    {{#checked}}
      <li class="option {{classes}} checked">
        <span class="checked">✓</span>
        {{content}}
      </li>
    {{/checked}}

    {{^checked}}
    <li class="option {{classes}}">
      {{content}}
    </li>
    {{/checked}}
  {{/menus}}
</ul>

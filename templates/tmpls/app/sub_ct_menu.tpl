{{#folders}}
  <li class="folder {{#selected}}selected{{/selected}}">
    {{ name }}
  </li>
{{/folders}}
<li class="splitter"></li>
<li class="new-folder">new folder</li>
<li class="splitter"></li>
<li class="unsubscribe">unsubscribe</li>
<li>
  <a href="{{sub.link}}" target="_blank">
    visite <span>{{sub.link}}</span>
  </a>
</li>

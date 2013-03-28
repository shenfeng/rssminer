{{#folders}}
  <li class="folder" data-title="{{m_move_to_folder}}{{name}}">
    {{ name }}
  </li>
{{/folders}}
<li class="splitter"></li>
<li class="new-folder">{{m_new_folder}}</li>
<li class="splitter"></li>
<li class="unsubscribe">{{m_unsubscribe}}</li>
<li>
  <a href="{{sub.link}}" target="_blank">
    {{m_visite}} <span>{{sub.link}}</span>
  </a>
</li>

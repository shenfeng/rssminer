<h2>
  {{#url}}
    <a href="{{url}}" target="_blank">{{ title }}</a>
  {{/url}}
  {{^url}}{{ title }}{{/url}}
</h2>
<div class="sort">
  <ul>
    {{#sort}}
      <li {{#selected}} class="selected" {{/selected}}>
        <a href="#{{href}}"> {{text}} </a>
      </li>
    {{/sort}}
  </ul>
</div>
{{>feeds}}

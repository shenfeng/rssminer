<div class="lfloat">
  <ul class="feeds">
    {{#feeds}}
      <li>
        <h3>{{title}}</h3>
        <p class="author">{{author}}</p>
        <p class="snippet">{{snippet}}</p>
        <ul class="categories">
          {{#categories}}
            <li>{{.}}</li>
          {{/categories}}
        </ul>
      </li>
    {{/feeds}}
  </ul>
</div>

<div class="rfloat">
  <ul class="tags">
    {{#tags}}
      <li>{{.}}</li>
    {{/tags}}
  </ul>
</div>


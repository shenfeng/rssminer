<h2>{{ title }}</h2>
{{#section}}
  <div class="section">
    <h4>{{title}}</h4>
    <ul class="feeds">
      {{#feeds}}
        <li class="feed {{cls}}">
          <a href="#{{href}}">
            <span class="indicator"></span>
            <span class="title" title="{{title}}">{{title}}</span>
            {{#author}}
              <span class="author">{{author}}</span>
            {{/author}}
            <span class="vote">
              <span class="up" title="I like it"></span>
              <span class="down"></span>
            </span>
            <ul class="tags">
              {{#tags}}
                <li>{{.}}</li>
              {{/tags}}
            </ul>
            <span class="date">{{ date }}</span>
          </a>
        </li>
      {{/feeds}}
    </ul>
  </div>
{{/section}}

<div class="section">
  {{#title}}<h4>{{title}}</h4>{{/title}}
    <ul class="feeds">
      {{#list}}
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
      {{/list}}
    </ul>
</div>

<ul>
  {{#feeds}}
    <li>
      <h4 title="{{title}}">
        <a href="{{href}}">{{title}}</a>
      </h4>
      <span class="date">{{date}}</span>
      <div class="vote-host">
        <span class="vote">
          <span class="up"></span>
          <span class="down"></span>
        </span>
        <span class="host">{{host}}</span>
      </div>
      <p class="snippet">
        <a href="{{href}}">{{snippet}}</a>
      </p>
    </li>
  {{/feeds}}
</ul>

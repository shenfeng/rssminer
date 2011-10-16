<ul id="charts">
  <li><div id="crawler-pie"></div></li>
  <li><div id="line-chart"></div></li>
</ul>
<ul>
  <li class="section">
    <h3>statistics</h3>
    <table>
      <tr>
        <th></th>
        <th>Count</th>
        <th>Delta</th>
      </tr>
      <tr>
        <td>Feed:</td>
        <td>{{feeds}}</td>
        <td>{{feeds_delta}}</td>
      </tr>
      <tr>
        <td>Rss:</td>
        <td>{{rss_links}} </td>
        <td>{{rss_links_delta}}</td>
      </tr>
      <tr>
        <td>Links:</td>
        <td>{{crawler_links}}</td>
        <td>{{crawler_links_delta}}</td>
      </tr>
      <tr>
        <td>Crawler counter:</td>
        <td>{{crawler_counter}}</td>
        <td>{{crawler_counter_delta}}</td>
      </tr>
      <tr>
        <td>Crawler speed:</td>
        <td>{{crawler_speed}} req/min</td>
        <td>{{crawler_speed_delta}}</td>
      </tr>
      <tr>
        <td>Crawler started:</td>
        <td colspan="2">{{interval crawler_start}}</td>
      </tr>
    </table>
  </li>
  <li class="section">
    <h3>server control</h3>
    <table id="controls">
      <tr data-sid="crawler">
        <td>Cralwer</td>
        <td class="status">{{crawler_running}}</td>
        <td><button>toggle</button></td>
      </tr>
      <tr data-sid="fetcher">
        <td>Fetcher</td>
        <td class="status">{{fetcher_running}}</td>
        <td><button>toggle</button></td>
      </tr>
      <tr data-sid="h2">
        <td>H2 server mode</td>
        <td class="status">{{h2}}</td>
        <td><button>toggle</button></td>
      </tr>
    </table>
  </li>
</ul>



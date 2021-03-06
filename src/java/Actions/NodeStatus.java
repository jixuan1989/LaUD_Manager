package Actions;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ActionContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.thrift.Cassandra.Client;

import Node.RingNode;
import Node.NodeInfo;
import Tools.ClusterConnection;
import Tools.ClusterManager;

import static org.junit.Assert.assertEquals;

/**
 * 该类用作处理获取所有节点信息的请求。
 * @author yeyh10
 */
public class NodeStatus extends ActionSupport {

    private ClusterManager clusterManager;
    private Client client;
    private ClusterConnection conn;

    public ClusterConnection getConn() {
        return this.conn;
    }

    public void setConn(ClusterConnection conn) {
        this.conn = conn;
    }
    
    private List<NodeInfo> infos = new ArrayList<NodeInfo>();

    public  ClusterManager getClusterManager() {
        return clusterManager;
    }

    public  void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public Client getClient() {
        return this.client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<NodeInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<NodeInfo> infos) {
        this.infos = infos;
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    public String execute() throws Exception {
        ActionContext actionContext = ActionContext.getContext();
        Map session = actionContext.getSession();
        ClusterConnection cn = (ClusterConnection)session.get("conn");
        cn.connect();
        setClusterManager(new ClusterManager(cn, cn.getHost(), cn.getJmxPort()));
        // 获取集群的节点环
        RingNode ring = getClusterManager().listRing();
        Map<Token, String> rangeMap = ring.getRangeMap();
        List<Token> t = ring.getRanges();
        for (Token token : t) {
            String primaryEndpoint = rangeMap.get(token);
            NodeInfo nodeInfo = null;
            try {
                // 获取该结点的详细信息
                nodeInfo = getClusterManager().getNodeInfo(primaryEndpoint, token);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertEquals(primaryEndpoint, nodeInfo.getEndpoint());
            infos.add(nodeInfo);
            session.put(nodeInfo.getEndpoint(), nodeInfo.getRange());
        }
        return "nodesinfo";
    }
}



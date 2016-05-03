package ssessner.com.cloudamqp;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.QueueingConsumer;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupConnectionFactory();

        final Handler incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                try {
                    JSONObject jsonMessage = new JSONObject(message);

                    JSONArray voltages = jsonMessage.getJSONArray("cell_voltages");

                    ProgressBar v1Progress = (ProgressBar) findViewById(R.id.v1progressBar);
                    TextView v1View = (TextView)findViewById(R.id.v1View);
                    double cell_1_voltage = voltages.getDouble(0);
                    int cell_1_percent = (int)(100*(cell_1_voltage - 3.2));

                    v1Progress.setProgress(cell_1_percent);
                    v1View.setText(String.format("%1$.3f", cell_1_voltage) + " V");

                    ProgressBar v2Progress = (ProgressBar) findViewById(R.id.v2progressBar);
                    TextView v2View = (TextView)findViewById(R.id.v2View);
                    double cell_2_voltage = voltages.getDouble(1);
                    int cell_2_percent = (int)(100*(cell_2_voltage - 3.2));

                    v2Progress.setProgress(cell_2_percent);
                    v2View.setText(String.format("%1$.3f", cell_2_voltage) + " V");

                    ProgressBar v3Progress = (ProgressBar) findViewById(R.id.v3progressBar);
                    TextView v3View = (TextView)findViewById(R.id.v3View);
                    double cell_3_voltage = voltages.getDouble(2);
                    int cell_3_percent = (int)(100*(cell_3_voltage - 3.2));

                    v3Progress.setProgress(cell_3_percent);
                    v3View.setText(String.format("%1$.3f", cell_3_voltage) + " V");

                    TextView current = (TextView) findViewById(R.id.currentView);
                    current.setText(jsonMessage.getString("charge_current") + " mA");

                    TextView modeView = (TextView) findViewById(R.id.statusView);
                    modeView.setText(jsonMessage.getString("mode"));

                }catch (JSONException ex){
                    ex.printStackTrace();
                }


                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss");
                //tv.setText(ft.format(now) + ' ' + message + '\n');
            }
        };
        subscribe(incomingMessageHandler);
    }

    Thread subscribeThread;
    ConnectionFactory factory = new ConnectionFactory();
    private void setupConnectionFactory() {
        String uri = "192.168.40.10";
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setHost(uri);

        //} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
        //    e1.printStackTrace();
        } catch (Exception e1){
            e1.printStackTrace();
        }
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        subscribeThread.interrupt();
    }

    void subscribe(final Handler handler)
    {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel channel = connection.createChannel();
                        channel.basicQos(1);
                        DeclareOk q = channel.queueDeclare();
                        channel.queueBind(q.getQueue(), "amq.fanout", "battery");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);

                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            String message = new String(delivery.getBody());
                            Log.d("","[r] " + message);
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e1) {
                        Log.d("", "Connection broken: " + e1.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }
}


package com.sowon.faceblindness_android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchActivity extends Fragment {
    private String encodings;
    private JSONObject jsonObject;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            encodings = ((MainActivity) getActivity()).passvalue();
            Log.d("encoding", encodings);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_search, container, false);

        Button snd_button = (Button) view.findViewById(R.id.test_send_btn);
        TextView tv = (TextView) view.findViewById(R.id.test_rcv_tv);
        if (encodings != null) {
            tv.setText(encodings);


            //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
            jsonObject = new JSONObject();

            try {
                jsonObject.accumulate("user_id", "soyoung");
                //jsonObject.accumulate("name", "why not working?");
                jsonObject.accumulate("name", encodings);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("json", "*********************JSON FILE Created************************");

        }

//        snd_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new JSONTask().execute("http://18.224.59.35:3000/post");// AsyncTask 시작
//                Log.d("connect", "*********************send http request************************");
//
//            }
//        });
        return view;
    }


//    public class JSONTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... urls) {
//            try {
//                Log.d("json", "*********************Action Start************************");
//
//
//                HttpURLConnection con = null;
//                BufferedReader reader = null;
//
//                try {
//                    //URL url = new URL("http://192.168.25.16:3000/users");
//                    URL url = new URL(urls[0]);
//                    //연결을 함
//                    con = (HttpURLConnection) url.openConnection();
//
//                    con.setRequestMethod("POST");//POST방식으로 보냄
//                    con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
//                    con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
//                    con.setRequestProperty("Accept", "text/html");//서버에 response 데이터를 html로 받음
//                    con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
//                    con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
//                    con.connect();
//                    Log.d("URL CONNECTION", "*********************success************************");
//
//
//                    //서버로 보내기위해서 스트림 만듬
//                    OutputStream outStream = con.getOutputStream();
//                    //버퍼를 생성하고 넣음
//                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
//                    writer.write(jsonObject.toString());
//                    writer.flush();
//                    writer.close();//버퍼를 받아줌
//
//                    //서버로 부터 데이터를 받음
//                    InputStream stream = con.getInputStream();
//
//                    reader = new BufferedReader(new InputStreamReader(stream));
//
//                    StringBuffer buffer = new StringBuffer();
//
//                    String line = "";
//                    while ((line = reader.readLine()) != null) {
//                        buffer.append(line);
//                    }
//
//                    return buffer.toString();//서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
//
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    if (con != null) {
//                        con.disconnect();
//                    }
//                    try {
//                        if (reader != null) {
//                            reader.close();//버퍼를 닫아줌
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            super.onPostExecute(result);
//        }
//    }

}


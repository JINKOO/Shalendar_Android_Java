package kr.ac.smu.cs.shalendar_java;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
/*
    Login하는 Activity
    Login -> MainActivity로 넘어간다.

 */
public class LoginActivity extends AppCompatActivity {

    //EditText 멤버 변수
    private EditText id_Email;
    private EditText password;

    //button 멤버 변수
    private Button buttonToMain;
    private Button buttonToMember1;

    //EditText값을 String에 저장하기 위한 멤버변수.
    private String userEmail;
    private String userPassword;

    //서버 통신 위한 url 객체 생성  여기서는 /signin
    private NetWorkUrl url = new NetWorkUrl();

    //서버로 부터 로그인 성공 시 오는 응답 Token 변수
    private String userToken;

    //서버로 부터 로그인 성공 시 오는 응답 UserName변수
    private String userName;

    //서버로 부터 로그인 성공 시 오는 응답 profileImageURL변수
    private String img_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        id_Email = findViewById(R.id.login_id_EditText);
        password = findViewById(R.id.login_password_EditText);


        buttonToMain = findViewById(R.id.login_toMain_button);
        buttonToMember1 = findViewById(R.id.login_signin_button);

        /*
           우선 버튼 클릭시 MainActivity로 넘어간다.
           - 나중에 CreateMemberActivity로 넘어가는 코드 짜야 한다.
        */
        //device TOken값
        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.i("Device Token", token);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //통신 준비.
        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);
        Ion.getDefault(this).getConscryptMiddleware().enable(false);

        //로그인 버튼
        buttonToMain.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                userEmail = id_Email.getText().toString().trim();
                userPassword = password.getText().toString().trim();

                Log.d("맞아?", Boolean.toString(userEmail.equals("jacob456@hanmail.net")));


                /*
                  입력하는 e-mail주소 형식 예외처리
                  사용자 이메일 & 비밀번호 dummy data
                  서버 닫혀 있을 때 서버 코드 주석처리 하고 아래  if else if else문 수행.

                if(userEmail.equals("jacob") && userPassword.equals("456")) {

                    Toast.makeText(getApplicationContext(), "사용자정보 일치 메인화면으로이동", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivityForResult(intent, CodeNumber.TO_MAIN_ACTIVITY);
                }

                else if(userEmail.equals("") || userPassword.equals("")) {
                    Toast.makeText(getApplicationContext(), "아이디 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                }

                else {
                    Log.d("입력한 ID", userEmail);
                    Log.d("입력한 비밀번호", userPassword);
                    Toast.makeText(getApplicationContext(), "아이디/비밀번호가 잘못되었습니다", Toast.LENGTH_SHORT).show();
                }
*/
                //서버 통신코드 Ion 롸이브뤄리 사용
                final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setMessage("로그인 중 입니다~");
                progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
                progressDialog.show();

                //응답 바디 설정.
                JsonObject json = new JsonObject();

                //응답 바디 서버에 보낼 data 넣음
                json.addProperty("id", userEmail);
                json.addProperty("pw", userPassword);
                Ion.with(getApplicationContext())
                        .load("POST", url.getServerUrl() + "/signin")
                        .setHeader("Content-Type", "application/json")
                        .progressDialog(progressDialog)
                        .setJsonObjectBody(json)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {

                                if( e!= null) {
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                else {
                                    progressDialog.dismiss();
                                    String message = result.get("message").getAsString();
                                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                    parseFromServer(message, result);
                                }
                            }
                        });
            }
        });

        //이메일로 회원가입 버튼 클릭 경우
        buttonToMember1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateMember1.class);
                startActivityForResult(intent, CodeNumber.TO_CREATE_MEMBER1);
            }
        });
    }
    //서버 응답 처리
    //응답으로 받은 userToken, getSharedPreference에 저장.
    public void parseFromServer(String message, JsonObject result) {
        if(message.equals("login success")) {

            if(result.get("img_url").isJsonNull()) {
                img_url = "DEFAULT :: profile_IMAGE";
            }
            else {
                img_url = result.get("img_url").getAsString();
            }

            userName = result.get("userName").getAsString();
            userToken = result.get("token").getAsString();

            Log.i("로그인시 받은 이미지 URL", img_url);

            SharedPreferences pref = getSharedPreferences("pref_USERTOKEN", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putString("userToken", userToken);
            editor.putString("userName", userName);
            editor.putString("userEmail", userEmail);
            editor.putString("img_url", img_url);
            editor.apply();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            intent.putExtra("userEmail", userEmail);
//            intent.putExtra("userName", userName);
//            intent.putExtra("img_url", img_url);
            startActivityForResult(intent, CodeNumber.TO_MAIN_ACTIVITY);
        }

        else if(message.equals("wrong password")) {
            Toast.makeText(getApplicationContext(), "사용자 정보가 일치 하지 않습니다", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "서버 연결 실패", Toast.LENGTH_LONG).show();
        }
    }
}
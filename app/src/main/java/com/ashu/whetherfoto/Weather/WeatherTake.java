package com.ashu.whetherfoto.Weather;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class WeatherTake {
    private String stWeatherResult;


    public String waetherTake(){
        WeatherAPI.ApiInterface api = WeatherAPI.getClient().create(WeatherAPI.ApiInterface.class);
        //Пока город берем хардкорно
        Double lat = 55.7507;// спб 59.9387;
        Double lng = 37.6177;//30.3162;
        String units = "metric";
        String key = WeatherAPI.KEY;

        // получаем погоду на сегодня
        Call<WeatherDay> callToday = api.getToday(lat, lng, units, key);
        try {
            //используем синхронный метод
            Response<WeatherDay> response = callToday.execute();
            WeatherDay data = response.body();

            //если получили ответ, то берем город и температуру
            if (response.isSuccessful()) {
                stWeatherResult = data.getCity() + " " + data.getTempWithDegree();

            }else {
                stWeatherResult = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stWeatherResult;
    }
}

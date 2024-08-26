package com.example.happyhunt.Model;

public class Favorite {
    private int id;
    private String placeName, placeAddress, type;

    public void setId(int id) {
        this.id = id;
    }
    public void setPlaceName(String placeName) {this.placeName = placeName;}
    public void setPlaceAddress(String placeAddress) {this.placeAddress = placeAddress;}
    public void setType(String type) {this.type = type;}
    public int getId() { return id; }
    public String getPlaceName() {return placeName;}
    public String getPlaceAddress() {return placeAddress;}
    public String getType() {return type;}

}

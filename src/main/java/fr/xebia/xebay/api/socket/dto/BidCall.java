package fr.xebia.xebay.api.socket.dto;

public class BidCall {

  private String itemName;
  private double curValue;
  private double increment;

  public BidCall(String itemName, double curValue, double increment) {
    this.itemName = itemName;
    this.curValue = curValue;
    this.increment = increment;
  }

  public String getItemName() {
    return itemName;
  }

  public double getCurValue() {
    return curValue;
  }

  public Double getIncrement() {
    return increment;
  }
}

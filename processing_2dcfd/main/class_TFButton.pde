class TFButton {
  int x, y;
  int wide, high;
  boolean b;
  String label, trueText, falseText;

  TFButton(int _x, int _y, boolean initValue, String _trueText, String _falseText, int _width, int _height) {
    x = _x;
    y = _y+ydim*3;
    wide = _width;
    high = _height;
    b = initValue;
    trueText = _trueText;
    falseText = _falseText;
    label = falseText;
    if (initValue==true) label = trueText;
  }

  void render() {
    fill(isOver()?128:64);
    rect(x, y, wide, high);
    textAlign(CENTER, CENTER);
    fill(240);
    text(label, x+wide/2, y+high/2);
  }

  void click() {
    if (isOver()) b = !b;

    if (b==true) label = trueText;
    else label = falseText;
  }

  boolean isOver() {
    return(mouseX>x && mouseX<x+wide && mouseY>y && mouseY<y+high);
  }
}

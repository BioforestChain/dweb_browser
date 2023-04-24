namespace DwebBrowser;

public partial class MainPage : ContentPage
{
	int count = 0;

	public MainPage()
	{
		InitializeComponent();
	}

	private void OnCounterClicked(object sender, EventArgs e)
	{
		count++;

		if (count == 1)
			CounterBtn.Text = String.Format("Clicked {0} time", count);
		else
			CounterBtn.Text = String.Format("Clicked {0} times", count);

		SemanticScreenReader.Announce(CounterBtn.Text);
	}
}



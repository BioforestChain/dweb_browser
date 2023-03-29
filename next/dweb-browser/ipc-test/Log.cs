using System;
using Xunit.Abstractions;

namespace ipc_test
{
    public class Log
    {
        public Log(ITestOutputHelper output)
        {
            Console.SetOut(new ConsoleWriter(output));
        }


        public class ConsoleWriter : StringWriter
        {
            private ITestOutputHelper output;
            public ConsoleWriter(ITestOutputHelper output)
            {
                this.output = output;
            }

            public override void WriteLine(string? m)
            {
                output.WriteLine($"{DateTime.Now.ToString("HH:mm:ss.fff")}\t| {m}");
            }
        }
    }
}


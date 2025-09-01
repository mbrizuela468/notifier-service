using WhatsAppService;
using WhatsAppService.Services;

var builder = Host.CreateApplicationBuilder(args);

var Services = builder.Services;

Services.AddHostedService<WorkerWhatsApp>();

Services.AddSingleton<WhatsAppSender>();

var host = builder.Build();
host.Run();

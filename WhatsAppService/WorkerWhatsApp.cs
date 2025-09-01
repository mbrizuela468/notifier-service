using WhatsAppService.Services;

namespace WhatsAppService
{
    public class WorkerWhatsApp : BackgroundService
    {
        private readonly ILogger<WorkerWhatsApp> _logger;
        private readonly WhatsAppSender _whatsAppService;

        public WorkerWhatsApp(ILogger<WorkerWhatsApp> logger, WhatsAppSender pWhatsAppSender)
        {
            _logger = logger;
            _whatsAppService = pWhatsAppSender;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Worker started at: {time}", DateTimeOffset.Now);

            try
            {
                string to = "whatsapp:+5493513483751";

                var messageId = _whatsAppService.SendMessage("¡Hola! Mensaje de prueba desde WorkerService", to);

                _logger.LogInformation("WhatsApp enviado. Message SID: {sid}", messageId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error enviando WhatsApp");
            }

            // Mantener el worker corriendo
            while (!stoppingToken.IsCancellationRequested)
            {
                await Task.Delay(10000, stoppingToken); // 10 segundos
            }
        }
    }
}

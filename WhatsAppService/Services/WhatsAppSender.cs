using Microsoft.Extensions.Configuration;
using Newtonsoft.Json;
using Twilio;
using Twilio.Rest.Api.V2010.Account;
using Twilio.Types;

namespace WhatsAppService.Services
{
    public class WhatsAppSender
    {
        private readonly string _accountSid;
        private readonly string _authToken;
        private readonly string _fromNumber;

        public WhatsAppSender(IConfiguration config)
        {
            _accountSid = config["Twilio:AccountSid"];
            _authToken = config["Twilio:AuthToken"];
            _fromNumber = config["Twilio:FromWhatsAppNumber"];
        }

        public string SendMessage(string pMessage, string pToNumber)
        {
            TwilioClient.Init(_accountSid, _authToken);

            var parameters = new Dictionary<string, string>()
            {
                { "first_name", "Chelo" },
                { "date", "12/1" },
                { "time", "3pm" },
            };

            var messageOptions = new CreateMessageOptions(
            new PhoneNumber(pToNumber));
            messageOptions.From = new PhoneNumber(_fromNumber);
            messageOptions.ContentSid = "HX9f81f8392c5c77b5979e5e8ee1285ad0";
            messageOptions.ContentVariables = JsonConvert.SerializeObject(parameters);


            var message = MessageResource.Create(messageOptions);
            Console.WriteLine(message.Body);

            return message.Sid;
        }
    }
}

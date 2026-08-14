package dataStructure;
public class S_LinkedList
{
    class Node
    {
        String data;
        Node next;
        Node(String data)
        {
            this.data=data;
            next=null;
        }
    }
    Node first_string = null;
    public void insertAtLast(String y)
    {
        Node n = new Node(y);
        if(first_string ==null)
        {
            first_string =n;
        }
        else
        {
            Node temp= first_string;
            while (temp.next!=null)
            {
                temp=temp.next;
            }
            temp.next=n;
        }
    }
    public void display()
    {
        int i=0;
        if(first_string ==null)
        {
            System.out.println("Empty");
        }
        else
        {
            Node temp= first_string;
            while(temp!=null)
            {
                System.out.println((++i)+"."+temp.data);
                temp = temp.next;

            }
        }
    }
    Node deleteAtFirst()
    {
        Node del= first_string;
        if(first_string ==null)
        {
            System.out.println("it's Empty");
            return del;
        }
        else
        {
            first_string = first_string.next;
            del.next=null;
            return del;
        }
    }
    public Node delete(String val)
    {
        System.out.println("string delete");
        Node dummy = first_string;
        boolean flag=false;
        while (dummy!=null)
        {
            if (dummy.data.equalsIgnoreCase(val))
            {
                flag=true;
                break;
            }
            dummy=dummy.next;
        }
        if (!flag)
        {
            System.out.println("no such target");
            return null;
        }
        else
        {
            Node temp = first_string;
            Node del;
            if (first_string.data.equalsIgnoreCase(val))
            {
                del=deleteAtFirst();
                return del;
            }
            else
            {
                while (!temp.next.data.equalsIgnoreCase(val))
                {
                    temp=temp.next;
                }
                del=temp.next;
                temp.next=del.next;
                del.next=null;
                return del;
            }
        }
    }
    public String findByPosition(int pos) {
        if (first_string == null) {
            return null;
        }

        if (pos <= 0) {
            //System.out.println("Invalid position (must be >= 1)");
            return null;
        }
        Node temp = first_string;
        int index = 1;

        while (temp != null) {
            if (index == pos) {
                //System.out.println("Value at position " + pos + " is: " + temp.data);
                return temp.data;
            }
            temp = temp.next;
            index++;
        }
        //System.out.println("Position " + pos + " does not exist in the list");
        return null;
    }
}


